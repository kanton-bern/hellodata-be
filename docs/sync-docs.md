
# Documentation Synchronization

We maintain our documentation in the main repository under the `/docs` (this folder). To facilitate the process of synchronizing just the `/docs` folder with the public repository, we use an `rsync`-based approach.

## Prerequisites

1. Both the main repository and the public repository should be cloned locally.
2. Ensure you have `rsync` installed on your system.

## Synchronization Process

1. **Initiate a Pull Request**: Before syncing, create a new branch in the public repository and push it to GitHub. This new branch will be the destination for our synchronization. Initiate a Pull Request (PR) on GitHub for this branch.

2. Navigate to your main repository's (internal) directory.
3. Run the `make sync-docs` command. This will:

   - Use `rsync` to synchronize the `/docs` folder from the main repo to the public repo on the new branch.
   - Commit the changes in the public repo.
   - Push the synchronized changes to the new branch in the public repo.

4. Manually add your git changes, commit and push. 

> [!WARNING]  
> We did not automate this step to avoid acidental push something. But below you have the command to do so.

```sh
git add docs/ && \
git commit -m "Sync docs from local repo" && \
git push origin main
```

5. Go back to the PR you initiated on the public repository's GitHub page. You'll see the synchronized changes. Review the PR, handle any conflicts, make any necessary adjustments, and then merge.

## Note

Please ensure that you are syncing in the right direction to avoid data loss. Always double-check before proceeding with the synchronization.

