plugins {
  id("org.metaborg.gradle.config") version "0.4.3"
  id("org.metaborg.gitonium") version "0.3.0"
  groovy
}

sourceSets {
  main {
    withConvention(GroovySourceSet::class) {
      groovy {
        setSrcDirs(listOf("src", "vars"))
      }
    }
  }

  test {
    withConvention(GroovySourceSet::class) {
      groovy {
        setSrcDirs(listOf("test"))
      }
    }
  }
}

dependencies {
  implementation("org.codehaus.groovy:groovy-all:2.5.5")
}
