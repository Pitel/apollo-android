plugins {
  kotlin("multiplatform")
}

configureMppDefaults()

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        api(projects.apolloApi)
        api(projects.apolloMppUtils)
        api(projects.apolloNormalizedCacheApi)
        api(groovy.util.Eval.x(project, "x.dep.kotlin.coroutines"))
      }
    }
  }
}
