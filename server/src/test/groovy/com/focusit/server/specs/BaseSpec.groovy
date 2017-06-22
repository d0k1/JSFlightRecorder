package com.focusit.server.specs

import spock.lang.Specification

/**
 * Created by Gallyam Biktashev on 15.02.17.
 */
abstract class BaseSpec extends Specification {
    def cleanupSpec() {
        new File('logs').deleteDir()
        new File('velocity.log').delete()
    }
}
