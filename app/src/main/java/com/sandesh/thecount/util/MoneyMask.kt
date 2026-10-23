package com.sandesh.thecount.util

object MoneyMask {
    const val HIDDEN = "XXXXX"
    inline fun display(hidden: Boolean, visible: () -> String): String = if (hidden) HIDDEN else visible()
}
