package com.gu.io.sttp

import com.gu.io.DownloaderSpec


abstract class SttpDownloaderSpec(downloader: SttpDownloader) extends DownloaderSpec(downloader)

class OkHttpSttpDownloaderSpec extends SttpDownloaderSpec(OkHttpSttpDownloader())
