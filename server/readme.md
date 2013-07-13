PowerGarden Server

	Software setup on server:
		Intall node from http://nodejs.org/

		Install homebrew:
			ruby -e "$(curl -fsSL https://raw.github.com/mxcl/homebrew/go)"
		Address any issues reported by homebrew's doctor command:
			brew doctor

		Install mongo:
			brew install mongodb
	
	Running & administering the Server:

		Log into SSH:
		ssh im_mini@10.0.1.2 (password is in credentials file)

		Check processes being run by forever:
		forever list

		Restart the server if something seems off:
		(this will only work if the `forever list` shows that it is running server.js)
		forever restartall

		Start server if it's not running
		i.e., `forever list` says no processes are running
		Preferred method is via the gui launcher:
		1. Screen share into machine
		2. On desktop, double click Power Garden Launcher
		3. Check that forever is now monitoring with `forever list`
		
		If this isn't working, launch it manually...
		cd ~/IncredibleMachines/powerGarden/server
		forever start -a -o logs/serverOut.log -l logs/server.log -e logs/serverError.log server.js

		View server log messages:
		tail -f ~/.forever/logs/server.log

		Connect to database shell
		mongo powergarden