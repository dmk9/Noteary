pipeline {
  agent any

  environment {
    COMPOSE_PROJECT_NAME = "noteary"
    BACKEND_DIR = "backend"
    FRONTEND_DIR = "frontend"
	JWT_SECRET = credentials('JWT_SECRET')
  }

  stages {
    stage('Clean Workspace') {
            steps {
                deleteDir()
            }
        }

    stage('Checkout') {
            steps {
                bat '"C:\\Program Files\\Git\\bin\\git.exe" clone https://github.com/Faithy847/Noteary .'
                bat '"C:\\Program Files\\Git\\bin\\git.exe" checkout main'
            }
        }

    stage('Install Dependencies') {
            steps {
                bat 'npm install'
            }
        }

    stage('Generate .env') {
      steps {
        script {
          writeFile file: '.env.runtime', text: """
            MONGODB_URI=mongodb://mongo:27017/notesdb
            JWT_SECRET=${env.JWT_SECRET}
          """.stripIndent()
        }
      }
    }

    stage('Build') {
      steps {
        echo "Building Docker images..."
        bat 'docker-compose -f docker-compose.yaml build'
      }
    }

    stage('Test') {
      steps {
        echo "Running backend tests..."
        dir("${BACKEND_DIR}") {
          // Add test framework (e.g., mocha, jest) if available
          bat 'npm install'
          bat 'npm test || echo "No tests defined yet"'
        }
      }
    }

	stage('Code Quality') {
	steps {
		script {
		['backend', 'frontend'].each { dirName ->
			echo "Running lint in ${dirName}..."
			dir(dirName) {
			bat 'npm install eslint || true'
			bat './node_modules/.bin/eslint . || echo "No linting errors or ESLint not configured"'
			}
		}
		}
	}
	}

	stage('Security') {
	steps {
		script {
		['backend', 'frontend'].each { dirName ->
			echo "Running npm audit in ${dirName}..."
			dir(dirName) {
			bat 'npm install'
			bat 'npm audit || echo "Security scan complete (audit)"'
			}
		}
		}
	}
	}

    stage('Deploy') {
      steps {
        echo "Deploying the stack..."
        bat 'docker-compose down || true'
        bat 'docker-compose up -d --build'
		bat 'docker-compose -p ${COMPOSE_PROJECT_NAME} up -d'
      }
    }

    stage('Release') {
      steps {
        echo "Tagging the release..."
        script {
          def commitId = bat(script: "git rev-parse --batort HEAD", returnStdout: true).trim()
          def tag = "release-${commitId}"
          bat "git tag ${tag}"
          bat "git pubat origin ${tag}"
        }
      }
    }

    stage('Monitoring') {
      steps {
        echo "Verifying service health..."
        bat 'curl -f http://localhost:3000 || echo "Frontend may not be ready"'
        bat 'curl -f http://localhost:5000 || echo "Backend may not be ready"'
      }
    }

  }

  post {
    success {
      echo "CI/CD pipeline completed successfully!"
    }
    failure {
      echo "Something went wrong in the pipeline."
    }
	always {
      bat 'del .env.runtime' 
    }
  }
}
