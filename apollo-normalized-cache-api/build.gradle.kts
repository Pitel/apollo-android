plugins {
  kotlin("multiplatform")
}

configureMppDefaults()

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        api(projects.apolloApi)
        implementation(groovy.util.Eval.x(project, "x.dep.okio"))
        api(groovy.util.Eval.x(project, "x.dep.uuid"))
      }
    }
  }
}

metalava {
  hiddenPackages += setOf("com.apollographql.apollo3.cache.normalized.internal")
}
