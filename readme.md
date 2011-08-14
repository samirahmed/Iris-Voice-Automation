[see the iris webpage]: http://www.samir-ahmed.com/iris.html
[contact me]: http://www.samir-ahmed.com
[iris.zip]: http://dl.dropbox.com/u/36801801/Iris.zip

# README: Iris Voice Automation

An interactive desktop assistant - uses voice commands to answer and automate simple lookup and playing tasks
For simple tasks like checking the weather and brief wikipedia queries.

-Utilizes the Google Speech API for voice recognition
-Utilizes the Google Translate API for speech synthesis

For more information [see the iris webpage] or [contact me]

## Sample  Questions

#### Who is Landon Donavan?	
	Landon Timothy Donovan is an American soccer player who plays for Los Angeles Galaxy in Major League Soccer.	

#### What is Vienna Philharmonic?
	The Vienna Philharmonic is an orchestra in Austria, regularly considered one of the finest in the world

#### What is the weather in San Francisco?
	Weather for san francisco, Currently Cloudy, at 60 degrees. Forecast says, Mostly Sunny. A high of 65 and A low of 54

## How To Build -ANT

To build Iris, clone the repository

	$ git clone git@github.com:samirahmed/Iris-Voice-Automation.git

Move into cloned directory,
Using ANT to build

	cd ./Iris-Voice-Automation
	ant compile jar

This will create a new folder called build with the jar in it

At the moment, the jar needs external files to run correctly

Extract the zip file "lib.zip" found in the folder

	 ./Iris-Voice-Automation/lib/lib.zip

The archived .zip lib.zip contains all the non source .jar dependencies

Copy this directory to the new build/jar folder

	./Iris-Voice-Automation/build/jar/

Now copy the res/ and misc/ folder to build/jar folder

After doing this you have all the necessary files for building.

Go to the build/jar directory and run the jar

	cd ./Iris-Voice-Automation/build/jar/
	java -jar Iris_0.1.0.jar
	
## Binary or Executable File

To get the latest binary / Executable file [Version 0.1]

- Download this [iris.zip] file
- Extract the zip
- Run the iris_0.1.jar file with the following command

	 $ java -jar iris_0.1.jar

## License Information

Iris Voice Automation created by Samir Ahmed is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 2 of the License, or
    (at your option) any later version.

    Iris Voice Automation is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Iris Voice Automation.  If not, see <http://www.gnu.org/licenses/>.
