PowerGarden Server

	Software setup on server:
		Intall node from http://nodejs.org/

		Install homebrew:
			ruby -e "$(curl -fsSL https://raw.github.com/mxcl/homebrew/go)"
		Address any issues reported by homebrew's doctor command:
			brew doctor

		Install mongo:
			brew install mongodb
	
	Running the Server:
		cd powerGarden/server
		npm install
		node server.js	
