
# HelloDATA BE Docs
* The documentation for HelloDATA is written in Markdown and is available here in the [docs folder](docs).
* These Markdown files are converted to static documentation HTML pages by means of [MkDocs](https://www.mkdocs.org).
* Conversion settings for MkDocs are configured in the [mkdocs.yml file](mkdocs.yml).

## Serve documentation on GitHub

* Currently, when something is pushed to the devel branch the documentation gets updated automatically ([see this GitHub Workflow](https://github.com/kanton-bern/hellodata-be/blob/develop/.github/workflows/deploy_docs.yml])) and is then available under https://kanton-bern.github.io/hellodata-be.

## Edit documentation on GitHub
* The documentation can be changed and pushed within GitHub but this approach should only be used to hot fix major errors.
* Generally we should handle documentation changes as usual within feature branches and with pull requests.
* Under [ocs/test/examples.md](docs/test/examples.md) you'll find several examples for Markdown constructs.  
## Manage and serve documentation locally

### Local installation
* To build and run the documentation locally you need to have Python 3 installed on your machine inclusive pip.
* Then run the following code once to create and activate your virtual environment and install MkDocs and its dependencies.

#### Linux/Mac
```bash
python -m venv ~/.venvs/hd-docs
source ~/.venvs/hd-docs/bin/activate
pip install mkdocs
pip install -r requirements.txt
``` 

#### Windows
```bash
python -m venv ~/.venvs/hd-docs
.\~/.venvs/hd-docs/Scripts/activate.bat
pip install mkdocs
pip install -r requirements.txt
```

### Serve documentation locally
* The following serves the documentation under http://127.0.0.1:8000 without creating the pages physically.

#### Linux/Mac
```bash
source ~/.venvs/hd-docs/bin/activate
mkdocs serve
```

#### Windows
```bash
.\~/.venvs/hd-docs/Scripts/activate.bat
mkdocs serve
```

### Build HTML pages locally
* The following generates static HTML pages under the folder `site`:

#### Linux/Mac
```bash
source ~/.venvs/hd-docs/bin/activate
mkdocs build
```

#### Windows
```bash
.\~/.venvs/hd-docs/Scripts/activate.bat
mkdocs build
```
