package org.odk.collect.android.formmanagement

import org.odk.collect.android.utilities.FileUtils.copyFile
import org.odk.collect.android.utilities.FileUtils.interuptablyWriteFile
import org.odk.collect.async.OngoingWorkListener
import org.odk.collect.forms.Form
import org.odk.collect.forms.FormSource
import org.odk.collect.forms.FormSourceException
import org.odk.collect.forms.FormsRepository
import org.odk.collect.forms.MediaFile
import org.odk.collect.shared.strings.Md5.getMd5Hash
import java.io.File
import java.io.IOException

class FormMediaDownloader(
    private val formsRepository: FormsRepository,
    private val formSource: FormSource
) {

    @Throws(IOException::class, FormSourceException::class, InterruptedException::class)
    fun download(
        formToDownload: ServerFormDetails,
        files: List<MediaFile>,
        tempMediaPath: String,
        tempDir: File,
        stateListener: OngoingWorkListener?
    ) {
        val tempMediaDir = File(tempMediaPath).also { it.mkdir() }

        for (i in files.indices) {
            stateListener?.progressUpdate(i + 1)

            val mediaFile = files[i]
            val tempMediaFile = File(tempMediaDir, mediaFile.filename)

            val existingFile = searchForExistingMediaFile(formToDownload, mediaFile)
            if (existingFile != null) {
                copyFile(existingFile, tempMediaFile)
            } else {
                val mediaFile = formSource.fetchMediaFile(mediaFile.downloadUrl)
                interuptablyWriteFile(mediaFile, tempMediaFile, tempDir, stateListener)
            }
        }
    }

    private fun searchForExistingMediaFile(
        formToDownload: ServerFormDetails,
        mediaFile: MediaFile
    ): File? {
        val allFormVersions = formsRepository.getAllByFormId(formToDownload.formId)
        return allFormVersions.map { form: Form ->
            File(form.formMediaPath, mediaFile.filename)
        }.firstOrNull { file: File ->
            val currentFileHash = getMd5Hash(file)
            val downloadFileHash = validateHash(mediaFile.hash)
            file.exists() && currentFileHash.contentEquals(downloadFileHash)
        }
    }

    private fun validateHash(hash: String?): String? {
        return if (hash == null || hash.isEmpty()) null else hash
    }
}
