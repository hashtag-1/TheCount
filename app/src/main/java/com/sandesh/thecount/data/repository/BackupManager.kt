
package com.sandesh.thecount.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.sandesh.thecount.data.db.DailyStrokeEntity
import com.sandesh.thecount.data.db.PaymentEntity
import com.sandesh.thecount.data.db.TheCountDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.math.BigDecimal
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class BackupMode {
    MERGE, REPLACE
}

data class BackupSummary(
    val strokeCount: Int,
    val paymentCount: Int,
    val firstDate: String?,
    val lastDate: String?,
    val sourceAccountId: String
)

data class ValidatedBackup internal constructor(
    val summary: BackupSummary,
    internal val strokes: List<BackupStroke>,
    internal val payments: List<BackupPayment>
)

internal data class BackupStroke(
    val id: UUID,
    val date: LocalDate,
    val count: Long
)

internal data class BackupPayment(
    val id: UUID,
    val date: LocalDate,
    val amount: BigDecimal,
    val note: String?
)

class BackupManager(
    private val context: Context,
    private val database: TheCountDatabase
) {
    companion object {
        private const val APP_NAME = "TheCount"
        private const val FORMAT_VERSION = 1
    }

    suspend fun exportTo(uri: Uri, userId: String) {
        require(userId.isNotBlank()) {
            "No signed-in account."
        }

        val strokeRows = database.dailyStrokeDao()
            .getBackupStrokes(userId)

        val paymentRows = database.paymentDao()
            .getBackupPayments(userId)

        val strokes = JSONArray()
        val payments = JSONArray()

        strokeRows.forEach { row ->
            strokes.put(
                JSONObject()
                    .put("id", row.id.toString())
                    .put("date", row.workDate.toString())
                    .put("count", row.strokeCount)
            )
        }

        paymentRows.forEach { row ->
            payments.put(
                JSONObject()
                    .put("id", row.id.toString())
                    .put("date", row.paymentDate.toString())
                    .put("amount", row.amount.setScale(2).toPlainString())
                    .put("note", row.note ?: JSONObject.NULL)
            )
        }

        val data = JSONObject()
            .put("strokes", strokes)
            .put("payments", payments)

        val dataString = data.toString()
        val checksum = sha256(dataString)

        val document = JSONObject()
            .put("appName", APP_NAME)
            .put("formatVersion", FORMAT_VERSION)
            .put("exportedAt", Instant.now().toString())
            .put("accountId", userId)
            .put("data", dataString)
            .put("checksum", checksum)

        // Supports both file:// URIs and Android Storage Access Framework URIs.
        val outputStream = if (uri.scheme == "file") {
            File(requireNotNull(uri.path)).outputStream()
        } else {
            context.contentResolver.openOutputStream(uri, "wt")
        }

        outputStream
            ?.bufferedWriter(Charsets.UTF_8)
            ?.use { writer ->
                writer.write(document.toString(2))
            }
            ?: error("Could not open the selected backup file.")
    }

    fun validateBackup(uri: Uri): ValidatedBackup {
        val raw = context.contentResolver.openInputStream(uri)
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            ?: error("Could not read the selected backup file.")

        val envelope = try {
            JSONObject(raw)
        } catch (e: Exception) {
            throw IllegalArgumentException("This file is not valid JSON.", e)
        }

        require(envelope.optString("appName") == APP_NAME) {
            "This backup was not created by TheCount."
        }

        require(envelope.optInt("formatVersion", -1) == FORMAT_VERSION) {
            "Unsupported backup format version."
        }

        val dataString = envelope.optString("data", "")
        require(dataString.isNotBlank()) {
            "Backup data is missing."
        }

        val expectedChecksum = envelope.optString("checksum", "")
        require(
            expectedChecksum.matches(Regex("[0-9a-f]{64}")) &&
                sha256(dataString) == expectedChecksum
        ) {
            "Backup checksum is invalid. The file may be damaged."
        }

        val accountId = envelope.optString("accountId", "")
        require(accountId.isNotBlank()) {
            "The backup account ID is missing."
        }

        val data = try {
            JSONObject(dataString)
        } catch (e: Exception) {
            throw IllegalArgumentException("The backup data is malformed.", e)
        }

        val strokeArray = data.optJSONArray("strokes")
            ?: error("Stroke list is missing.")

        val paymentArray = data.optJSONArray("payments")
            ?: error("Payment list is missing.")

        val strokes = mutableListOf<BackupStroke>()
        val strokeDates = mutableSetOf<LocalDate>()
        val strokeIds = mutableSetOf<UUID>()
        val today = LocalDate.now()

        for (i in 0 until strokeArray.length()) {
            try {
                val item = strokeArray.getJSONObject(i)
                val date = BackupRules.parseDate(item.getString("date"))
                BackupRules.validateDate(date, today)
                val count = item.getLong("count")
                BackupRules.validateStrokeCount(count)
                val id = UUID.fromString(item.getString("id"))
                BackupRules.addUniqueStrokeDate(date, strokeDates)
                BackupRules.addUniqueId(id, strokeIds)
                strokes += BackupStroke(id, date, count)
            } catch (e: Exception) {
                throw IllegalArgumentException("This backup file is damaged or not valid.", e)
            }
        }

        val payments = mutableListOf<BackupPayment>()
        val paymentIds = mutableSetOf<UUID>()

        for (i in 0 until paymentArray.length()) {
            try {
                val item = paymentArray.getJSONObject(i)
                val date = BackupRules.parseDate(item.getString("date"))
                BackupRules.validateDate(date, today)
                val amountString = item.getString("amount")
                val id = UUID.fromString(item.getString("id"))
                val amount = BackupRules.parsePaymentAmount(amountString)
                BackupRules.addUniqueId(id, paymentIds)
                val note = if (item.isNull("note")) null else item.getString("note")
                payments += BackupPayment(id, date, amount, note)
            } catch (e: Exception) {
                throw IllegalArgumentException("This backup file is damaged or not valid.", e)
            }
        }

        val allDates = strokes.map { it.date } +
            payments.map { it.date }

        return ValidatedBackup(
            summary = BackupSummary(
                strokeCount = strokes.size,
                paymentCount = payments.size,
                firstDate = allDates.minOrNull()?.toString(),
                lastDate = allDates.maxOrNull()?.toString(),
                sourceAccountId = accountId
            ),
            strokes = strokes,
            payments = payments
        )
    }

    suspend fun importBackup(
        backup: ValidatedBackup,
        currentUserId: String,
        mode: BackupMode,
        confirmedAccountMismatch: Boolean = false,
        replaceConfirmation: String = ""
    ) {
        require(currentUserId.isNotBlank()) {
            "Sign in before importing a backup."
        }

        if (backup.summary.sourceAccountId != currentUserId) {
            require(confirmedAccountMismatch) {
                "This backup belongs to a different account. Confirmation is required."
            }
        }

        if (mode == BackupMode.REPLACE) {
            require(replaceConfirmation == "REPLACE") {
                "Type REPLACE to confirm replacement."
            }

            // Save a recovery copy before modifying the database.
            val automaticBackup = File(
                context.cacheDir,
                "thecount-before-replace-${System.currentTimeMillis()}.json"
            )

            exportTo(Uri.fromFile(automaticBackup), currentUserId)
        }

        database.withTransaction {
            val strokeDao = database.dailyStrokeDao()
            val paymentDao = database.paymentDao()
            val now = System.currentTimeMillis()

            val importedStrokeDates = backup.strokes
                .map { it.date }
                .toSet()

            if (mode == BackupMode.REPLACE) {
                strokeDao.getAllStrokesForBackup(currentUserId)
                    .filter {
                        it.deletedAt == null &&
                            it.workDate !in importedStrokeDates
                    }
                    .forEach { existing ->
                        strokeDao.update(
                            existing.copy(
                                deletedAt = now,
                                updatedAt = now,
                                syncState = "PENDING_DELETE"
                            )
                        )
                    }
            }

            for (item in backup.strokes) {
                val existing = strokeDao.getByDate(
                    currentUserId,
                    item.date
                )

                if (existing == null) {
                    strokeDao.insert(
                        DailyStrokeEntity(
                            id = item.id,
                            userId = currentUserId,
                            workDate = item.date,
                            strokeCount = item.count,
                            createdAt = now,
                            updatedAt = now,
                            version = 0,
                            syncState = "PENDING_UPSERT"
                        )
                    )
                } else if (
                    BackupRules.shouldApplyImportedRecord(
                        mode,
                        existing.deletedAt != null
                    )
                ) {
                    strokeDao.update(
                        existing.copy(
                            strokeCount = item.count,
                            updatedAt = now,
                            syncState = "PENDING_UPSERT",
                            deletedAt = null
                        )
                    )
                }
            }

            val importedPaymentIds = backup.payments
                .map { it.id }
                .toSet()

            if (mode == BackupMode.REPLACE) {
                paymentDao.getAllPaymentsForBackup(currentUserId)
                    .filter {
                        it.deletedAt == null &&
                            it.id !in importedPaymentIds
                    }
                    .forEach { existing ->
                        paymentDao.update(
                            existing.copy(
                                deletedAt = now,
                                updatedAt = now,
                                syncState = "PENDING_DELETE"
                            )
                        )
                    }
            }

            for (item in backup.payments) {
                val existing = paymentDao.getById(
                    currentUserId,
                    item.id
                )

                if (existing == null) {
                    paymentDao.insert(
                        PaymentEntity(
                            id = item.id,
                            userId = currentUserId,
                            paymentDate = item.date,
                            amount = item.amount,
                            note = item.note,
                            createdAt = now,
                            updatedAt = now,
                            version = 0,
                            syncState = "PENDING_UPSERT"
                        )
                    )
                } else if (
                    BackupRules.shouldApplyImportedRecord(
                        mode,
                        existing.deletedAt != null
                    )
                ) {
                    paymentDao.update(
                        existing.copy(
                            paymentDate = item.date,
                            amount = item.amount,
                            note = item.note,
                            updatedAt = now,
                            syncState = "PENDING_UPSERT",
                            deletedAt = null
                        )
                    )
                }
            }
        }
    }

    private fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))

        return bytes.joinToString("") {
            "%02x".format(it.toInt() and 0xff)
        }
    }
}
