/**
 * Deploy the built openshift images
 */

// Registering decommissioning poststep
MPLPostStep('always') {
  echo "OpenShift Deploy Decomission poststep"
}

echo 'Executing Openshift Deploy process'
