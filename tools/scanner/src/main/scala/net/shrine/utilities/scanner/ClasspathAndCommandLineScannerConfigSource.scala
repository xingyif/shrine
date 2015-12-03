package net.shrine.utilities.scanner

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

/**
 * @author clint
 * @date Nov 21, 2013
 */
object ClasspathAndCommandLineScannerConfigSource {
  def config(commandLineProps: CommandLineScannerConfigParser): ScannerConfig = {
    val fromConfigFiles = ConfigFactory.load
    
    val fromCommandLine = commandLineProps.toTypesafeConfig
    
    def hasDuration(config: Config) = config.hasPath(ScannerConfig.ScannerConfigKeys.reScanTimeout) && !config.getConfig(ScannerConfig.ScannerConfigKeys.reScanTimeout).isEmpty
      
    //
    //If both configs have rescan timeout info, remove the one from the classpath to avoid clashes, whcih can occur when merging things like
    // scanner.reScanTimeout.seconds = 42
    //and 
    //scanner.reScanTimeout.minutes = 42
    //which produces
    //scanner.reScanTimeout {
    //  seconds = 42
    //  minutes = 42
    //}
    val mungedFromConfigFiles = {
      if(hasDuration(fromCommandLine) && hasDuration(fromConfigFiles)) {
	    fromConfigFiles.withoutPath(ScannerConfig.ScannerConfigKeys.reScanTimeout)
	  } else {
	    fromConfigFiles
	  }
    }
    
    ScannerConfig(fromCommandLine.withFallback(mungedFromConfigFiles))
  }
}