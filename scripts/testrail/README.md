# TestRail

This folder contains the [testrail.py](./testrail.py) script that is used to upload test results directly to TestRail.

## Upload Protractor Test Results

1. Create an API Key in [TestRail](https://cae-testrail.jpl.nasa.gov/testrail/index.php?/mysettings) in your TestRail user settings.
   1. Copy the key (**SAVE IT**, you won't be able to access it later on)
   2. Click add key
   3. **Click `Save Settings` or else your API key won't be active**
2. Set `TESTRAIL_USERNAME` environment variable with your jpl email and `TESTRAIL_API_KEY` with your api key that you made.
3. We will be running this script in a [Python virtual environment](https://realpython.com/python-virtual-environments-a-primer/)
   1. Run the following commands in [./scripts/testrail](.)
   2. `python3 -m venv env`
   3. `source env/bin/activate`
   4. `pip install -r requirements.txt`
      - This installs the script dependencies. You only need to run this once.
4. Now you can run `python3 main.py path/to/your/testouptut.xml <section_id>` where section_id is the id of the section in testrail that you want your test results to be uploaded to.
5. If you want to exit the virtual environment, just type `deactivate`
