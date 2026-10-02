package us.bringardner.core.test;

/**
 * Used by TestPluginProperties: it is loaded again by a separate class loader, which is
 * the only place its properties file exists (there is deliberately none on the class path).
 */
public class IsolatedPropertyTarget {
}
