// Merges the provided configuration with the pipeline config

import com.griddynamics.devops.mpl.MPLManager

def call(cfg) {
  MPLManager.instance.configMerge(cfg)
}
