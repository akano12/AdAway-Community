# The preference screens name these fragments in XML only, which R8 cannot see
# See https://issuetracker.google.com/issues/145316223
-keep public class org.adaway.ui.prefs.PrefsBackupRestoreFragment
-keep public class org.adaway.ui.prefs.PrefsUpdateFragment

-dontobfuscate

### dnsjava ###
# Java 18+ InetAddressResolverProvider, only in the jar's META-INF/versions/18 and registered as a
# service: Android has neither the interface nor the service lookup
-dontwarn java.net.spi.InetAddressResolverProvider
-dontwarn org.xbill.DNS.spi.DnsjavaInetAddressResolverProvider
# Compile-only Lombok annotation, and the pre-Java 9 name service it also registers
-dontwarn lombok.Generated
-dontwarn sun.net.spi.nameservice.NameServiceDescriptor
