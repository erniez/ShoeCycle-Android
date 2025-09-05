package com.shoecycle.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Utility class for handling file sharing through FileProvider
 * Required for sharing files on Android 7.0+ (API 24+)
 */
class FileProviderUtility {
    
    companion object {
        const val AUTHORITY_SUFFIX = ".fileprovider"
        const val MIME_TYPE_CSV = "text/csv"
        const val MIME_TYPE_EMAIL = "message/rfc822"
    }
    
    /**
     * Gets a content URI for a file using FileProvider
     * @param context Application context
     * @param file The file to get URI for
     * @return Content URI that can be shared with other apps
     */
    fun getUriForFile(context: Context, file: File): Uri {
        val authority = "${context.packageName}$AUTHORITY_SUFFIX"
        return FileProvider.getUriForFile(context, authority, file)
    }
    
    /**
     * Creates an email intent with CSV attachment
     * @param context Application context
     * @param csvFile The CSV file to attach
     * @param shoe The shoe being exported (for email metadata)
     * @return Intent configured for sending email with attachment
     */
    fun createEmailIntent(
        context: Context,
        csvFile: File,
        shoeBrand: String
    ): Intent {
        val fileUri = getUriForFile(context, csvFile)
        
        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = MIME_TYPE_CSV
            
            // Email configuration
            putExtra(Intent.EXTRA_SUBJECT, "CSV data from ShoeCycle shoe: $shoeBrand")
            putExtra(Intent.EXTRA_TEXT, "Attached is the CSV shoe data from ShoeCycle!")
            
            // Attach the CSV file
            putExtra(Intent.EXTRA_STREAM, fileUri)
            
            // Grant temporary read permission to email app
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        // Create chooser to let user select email app
        return Intent.createChooser(emailIntent, "Send shoe data via email")
    }
    
    /**
     * Checks if there's an app available to handle email
     * @param context Application context
     * @return true if email can be sent, false otherwise
     */
    fun canSendEmail(context: Context): Boolean {
        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = MIME_TYPE_EMAIL
        }
        return emailIntent.resolveActivity(context.packageManager) != null
    }
    
    /**
     * Creates a share intent for the CSV file (general sharing, not just email)
     * @param context Application context
     * @param csvFile The CSV file to share
     * @return Intent configured for sharing the file
     */
    fun createShareIntent(context: Context, csvFile: File): Intent {
        val fileUri = getUriForFile(context, csvFile)
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = MIME_TYPE_CSV
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        return Intent.createChooser(shareIntent, "Share shoe history data")
    }
}