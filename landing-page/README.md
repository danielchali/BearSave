# BearTube landing page

This is a standalone static site for BearTube. It uses plain HTML and CSS, includes its own assets, and needs no package manager, build step, server-side code, or third-party runtime requests.

## Preview locally

From the repository root, run:

```sh
python3 -m http.server 8080 --directory landing-page
```

Then open `http://localhost:8080`.

## Publish with GitHub Pages

GitHub Pages' branch-based source serves the repository root or `/docs`; it does not publish an arbitrary `landing-page/` subdirectory directly. To keep this directory as-is, set the repository's Pages source to **GitHub Actions** and configure a Pages workflow that uploads the contents of `landing-page/` as the Pages artifact, then deploys that artifact with GitHub's Pages deploy action. No compilation step is needed. Alternatively, copy the directory contents into the selected branch source directory.

All local page assets use paths relative to this directory (for example, `assets/beartube-icon.png`), so they work at both a custom-domain root and a GitHub project-page path. The download, release, source, license, and issue links point to BearTube's canonical GitHub repository.
