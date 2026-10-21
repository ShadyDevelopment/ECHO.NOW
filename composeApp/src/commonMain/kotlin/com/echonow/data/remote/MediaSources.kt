package com.echonow.data.remote

import com.echonow.domain.model.MediaType

fun MediaType.toProviderType(): String =
    if (this == MediaType.MOVIE) "movie" else "series"
