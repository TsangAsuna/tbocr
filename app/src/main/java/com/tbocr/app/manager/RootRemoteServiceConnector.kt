package com.tbocr.app.manager

import com.tbocr.app.domain.models.RemoteBackend
import com.tbocr.app.remote.RemoteServiceImpl

object RootRemoteServiceConnector : ProcessServiceConnectorBackend(SuSpawner) {

    override val backend = RemoteBackend.ROOT
    override val eventPrefix = "ROOT"
    override val processNameSuffix = "root_service"
    override val serviceClass: Class<*> = RemoteServiceImpl::class.java
    override val logFileName = "root_launch_debug.log"
}
