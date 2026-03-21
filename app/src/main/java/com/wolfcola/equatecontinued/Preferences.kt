package com.wolfcola.equatecontinued

import org.json.JSONObject

class Preferences {
    var percentButMain: String = "%"
        private set
    var percentButSec: String = "EE"
        private set

    constructor()

    constructor(json: JSONObject) {
        percentButMain = json.getString(JSON_PERCENT_BUT_MAIN)
        percentButSec = json.getString(JSON_PERCENT_BUT_SEC)
    }

    fun toJSON(): JSONObject {
        return JSONObject().apply {
            put(JSON_PERCENT_BUT_MAIN, percentButMain)
            put(JSON_PERCENT_BUT_SEC, percentButSec)
        }
    }

    fun setPercentButMain(value: String) {
        percentButMain = value
    }

    fun setPercentButSec(value: String) {
        percentButSec = value
    }

    companion object {
        private const val JSON_PERCENT_BUT_MAIN = "percent_main"
        private const val JSON_PERCENT_BUT_SEC = "percent_sec"
    }
}
