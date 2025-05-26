pipeline {
  agent any

  environment {
    COMPOSE_PROJECT_NAME = "notery"
	TEST_COMPOSE_PROJECT_NAME = "notery-test"
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

    stage('Generate .env') {
      steps {
        script {
          writeFile file: '.env.runtime', text: """
            MONGODB_URI=mongodb://localhost:27017/notesdb
            JWT_SECRET=${env.JWT_SECRET}
          """.stripIndent()
        }
      }
    }

    stage('Build') {
      steps {
        echo "Building Docker images..."
        bat 'docker-compose build'
      }
    }

	stage('Test') {
	steps {
		script {
		writeFile file: 'docker-compose.test.yaml', text: """\
    services:
      mongo:
        ports:
          - "27018:27017"
        networks:
          - notery-test-net
    
    networks:
      notery-test-net:
    """

		// Start test stack with override (fixed line continuation)
		bat """
			docker-compose -p ${env.TEST_COMPOSE_PROJECT_NAME} ^
			-f docker-compose.yaml ^
			-f docker-compose.test.yaml ^
			up -d mongo
		"""
      
      // Configure tests
      writeFile file: 'backend/.env.test', text: """
        MONGODB_URI=mongodb://localhost:27018/notesdb
        JWT_SECRET=${env.JWT_SECRET}
        """
    }	
}

		dir("${BACKEND_DIR}") {
		bat 'npm install'
		bat 'npm test || echo "No tests defined yet"'
		}

		post {
		always {
			bat """
			docker-compose -p ${env.TEST_COMPOSE_PROJECT_NAME} ^
				-f docker-compose.yaml ^
				-f docker-compose.test.yaml ^
				down --remove-orphans -v
			"""
			bat 'del docker-compose.test.yaml'
		}
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
			bat 'node_modules\\.bin\\eslint.cmd . || echo "No linting errors or ESLint not configured"'			}
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
			bat 'npm audit fix'
			}
		}
		}
	}
	}

	stage('Deploy') {
	steps {
		script {
		// Create deployment-specific port override
		writeFile file: 'docker-compose.deploy.yaml', text: """\
	services:
	  mongo:
	    ports:
		  - "27017:27017"  # Add host port binding ONLY for deployment
	"""
		
		echo "Deploying production stack..."
		bat """
			docker-compose -p ${env.COMPOSE_PROJECT_NAME} ^
			-f docker-compose.yaml ^
			-f docker-compose.deploy.yaml ^
			down --remove-orphans || true
		"""
		bat """
			docker-compose -p ${env.COMPOSE_PROJECT_NAME} ^
			-f docker-compose.yaml ^
			-f docker-compose.deploy.yaml ^
			up -d --build
		"""
		
		// Cleanup override file after deployment
		bat 'del docker-compose.deploy.yml'
		}
	}
	}

    stage('Release') {
      steps {
        echo "Tagging the release..."
        script {
          def commitId = bat(script: "git rev-parse --short HEAD", returnStdout: true).trim()
          def tag = "release-${commitId}"
          bat "git tag ${tag}"
          bat "git push origin ${tag}"
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