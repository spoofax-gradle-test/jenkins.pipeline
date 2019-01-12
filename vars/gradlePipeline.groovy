def call(Map args) {
  def publish
  def publishTaggedOnly
  def gradleRefreshDependencies

  pipeline {
    agent any

    triggers {
      upstream(upstreamProjects: args?.upstreamProjects != null ? args.upstreamProjects.join(",") : "", threshold: hudson.model.Result.SUCCESS)
    }

    environment {
      JENKINS_NODE_COOKIE = 'dontKillMe' // Necessary for the Gradle daemon to be kept alive.
    }

    stages {
      stage('Prepare') {
        steps {
          script {
            def defaultProps = [
              'publish'                      : args?.publish != null ? args.publish : true
              , 'publish.tagged.only'        : args?.publishTaggedOnly != null ? args.publishTaggedOnly : (BRANCH_NAME == "master")
              , 'gradle.refresh.dependencies': args?.gradleRefreshDependencies != null ? args.gradleRefreshDependencies : (args.upstreamProjects != null && !args.upstreamProjects.length() > 0)
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
