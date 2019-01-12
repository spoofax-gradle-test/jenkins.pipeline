def call(Map args) {
  def publish = args?.publish
  def publishTaggedOnly = args?.publishTaggedOnly
  def gradleRefreshDependencies = args?.gradleRefreshDependencies
  def upstreamProjects = args?.upstreamProjects
  def hasUpstreamProjects = upstreamProjects != null && upstreamProjects.length() > 0

  pipeline {
    agent any

    triggers {
      upstream(upstreamProjects: hasUpstreamProjects ? upstreamProjects.join(",") : "", threshold: hudson.model.Result.SUCCESS)
    }

    environment {
      JENKINS_NODE_COOKIE = 'dontKillMe' // Necessary for the Gradle daemon to be kept alive.
    }

    stages {
      stage('Prepare') {
        steps {
          script {
            def defaultProps = [
              'publish'                      : publish ?: true
              , 'publish.tagged.only'        : publishTaggedOnly ?: (BRANCH_NAME == "master")
              , 'gradle.refresh.dependencies': gradleRefreshDependencies ?: hasUpstreamProjects
            ]
            def props = readProperties defaults: defaultProps, file: 'jenkins.properties'
            publish = props['publish'] == 'true'
            publishTaggedOnly = props['publish.tagged.only'] == 'true'
            gradleRefreshDependencies = props['gradle.refresh.dependencies'] == 'true'
          }
        }
      }

      stage('Refresh dependencies') {
        when { expression { return gradleRefreshDependencies } }
        steps {
          sh 'gradle --refresh-dependencies'
        }
      }

      stage('Build') {
        steps {
          sh 'gradle build'
        }
      }

      stage('Publish') {
        when {
          expression { return publish }
          anyOf {
            not { expression { return publishTaggedOnly } }
            allOf { expression { return publishTaggedOnly }; tag "*release-*" }
          }
        }
        steps {
          withCredentials([usernamePassword(credentialsId: 'artifactory', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
            sh 'gradle publish -Ppublish.repository.Artifactory.username=$USERNAME -Ppublish.repository.Artifactory.password=$PASSWORD'
          }
        }
      }
    }
  }
}
