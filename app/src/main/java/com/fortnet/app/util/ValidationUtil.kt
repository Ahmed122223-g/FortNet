package com.fortnet.app.util

object ValidationUtil {
    // النمط الرسمي لاسم الحزمة في أندرويد
    private val PACKAGE_NAME_REGEX = Regex("^[a-z][a-z0-9_]*(\\.[a-z0-9_]+)+[0-9a-z_]\$")

    fun isValidPackageName(packageName: String): Boolean {
        return packageName.isNotBlank() && PACKAGE_NAME_REGEX.matches(packageName)
    }
}
