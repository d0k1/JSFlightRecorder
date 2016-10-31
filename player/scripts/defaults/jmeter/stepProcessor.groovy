logger.info("Proceeding with hash " + System.identityHashCode(sampler));

logger.info("Sample passed " + sampler.getName() + " Response code " + sample.getResponseCode() + " hash " + System.identityHashCode(sampler));

return true;