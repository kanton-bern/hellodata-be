.DEFAULT_GOAL := serve

serve: #run docs
	( \
       source ~/.venvs/hd-docs/bin/activate; \
       mkdocs serve; \
    )

build: #build docs as HTML page under `site` folder
	( \
       source ~/.venvs/hd-docs/bin/activate; \
       mkdocs build; \
       docker build -t bedag/hd-docs:latest --platform "linux/amd64" .; \
       docker push bedag/hd-docs:latest; \
    )

install: #run ones to create and activate your virtual environment and install dependencies
	( \
			 python -m venv ~/.venvs/hd-docs; \
       source ~/.venvs/hd-docs/bin/activate; \
       pip install -r requirements.txt; \
    )

prepare: ## prepare commands
	rm -rf site
	echo "site deleted.."

:

deploy-intern:
	kubectl delete -f deployment/mkdocs-deplyoment.yaml > /dev/null 2>&1 || true
	kubectl delete -f deployment/mkdocs.svc.yaml > /dev/null 2>&1 || true
	kubectl delete -f deployment/mkdocs.ingress.yaml > /dev/null 2>&1 || true
	kubectl apply -f deployment/mkdocs-deplyoment.yaml
	kubectl apply -f deployment/mkdocs.svc.yaml
	kubectl apply -f deployment/mkdocs.ingress.yaml

build-and-deploy: prepare build #deploy

# Paths:
SRC_BASE_DIR := ../
SRC_DIR := .

DEST_DIR := ~/Documents/git/bedag/kaio/hellodata-be/docs/
PUBLIC_REPO_DIR := ~/Documents/git/bedag/kaio/hellodata-be/ #TODO: replace with ENVs: $(HELLO_DATA_BE_GIT)

ROOT_FILES := $(addprefix $(SRC_BASE_DIR),CODE-OF-CONDUCT.md CONTRIBUTING.md LICENSE README.md THIRD-PARTY-LICENSES.md .github)

# Sync docs from the internal to the public repo
sync-docs:
	@echo "Synchronizing /docs..."
	rsync -a --delete $(SRC_DIR)/ $(DEST_DIR)
	@echo "Synchronizing root files..."
	rsync -a $(ROOT_FILES) $(PUBLIC_REPO_DIR)
	@echo "Synchronization to local public repo complete. Review local changes, commit/push, and merge the PR on GitHub."
	@echo "running `git status` to show changes..."
	cd $(PUBLIC_REPO_DIR) && rm docs/sync-docs.md && rm -r docs/docs/.obsidian && git status
	##
	# git add docs/ && \
	# git commit -m "Sync docs from local repo" && \
	# git push origin main

help: ## Show all Makefile targets
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'


.PHONY: sync-docs
