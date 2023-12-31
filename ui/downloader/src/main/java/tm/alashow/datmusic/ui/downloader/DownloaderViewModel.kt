/*
 * Copyright (C) 2021, Alashov Berkeli
 * All rights reserved.
 */
package tm.alashow.datmusic.ui.downloader

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import tm.alashow.datmusic.downloader.Downloader
import javax.inject.Inject

@HiltViewModel
internal class DownloaderViewModel @Inject constructor(val downloader: Downloader) : ViewModel()
