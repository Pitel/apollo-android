plugins {
  kotlin("jvm")
}

dependencies {
  implementation(projects.apolloApi)
  api(groovy.util.Eval.x(project, "x.dep.rx2"))
  api(groovy.util.Eval.x(project, "x.dep.kotlin.coroutinesRx2"))

  api(projects.apolloRuntime)
  api(projects.apolloNormalizedCache)
}


