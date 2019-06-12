# TestRail

This folder contains the [testrail.py](./testrail.py) script that is used to upload test results directly to TestRail.

## Upload Protractor Test Results

1. Create an API Key in [TestRail](https://cae-testrail.jpl.nasa.gov/testrail/index.php?/mysettings) in your TestRail user settings.
   1. Copy the key (**SAVE IT**, you won't be able to access it later on)
   2. Click add key
   3. **Click `Save Settings` or else your API key won't be active**
2. Follow the directions in [config/creds-template](./config/creds-template).
3. We will be running this script in a [Python virtual environment](https://realpython.com/python-virtual-environments-a-primer/)
   1. Run the following commands in [./nest/scripts/testrail](.)
   2. `python3 -m venv env`
   3. `source env/bin/activate`
   4. `pip install -r requirements.txt`
      - This installs the script dependencies. You only need to run this once.
4. Now you can run `npm run e2e` and `npm run e2e:upload <run-id>` in `/nest`
5. `run-id` is a TestRail "Test Runs" `id`. Find your test run [here](https://cae-testrail.jpl.nasa.gov/testrail/index.php?/runs/overview/20).
6. If you want to exit the virtual environment, just type `deactivate`
