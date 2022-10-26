# Vale configuration

We use Vale https://github.com/errata-ai/vale a grammar, style and word usage linter for the English language.

## Configuration

### Configuration file

Vale's configuration is stored in the `.vale.ini` file located in the root directory.

### Styles

Our Vale [styles](https://docs.errata.ai/vale/styles) are stored in the [styles](styles) directory. These styles get packaged together and are enforced by the Vale CLI.

Words normally flagged by Vale that we wish to use anyway are stored in [styles/Vocab/Base/accept.txt](styles/Vocab/Base/accept.txt).

## Vale in CI using GitHub Actions

We run Vale in CI using the open source [GitHub Action](https://github.com/marketplace/actions/vale-linter). Configuration is stored in `.github/workflows/linting.yml`. The `MinAlertLevel` set in `.vale.ini` determines the alert level at which the GitHub Action will fail a build.

## Running locally

To run Vale locally:

macOS
```bash
gem install asciidoctor
brew install vale
vale docs
```

When writing new sections of documentation, we recommend running Vale to see linting alerts at `warning` or `suggestion` level. The alerts might provide suggestions on how to make your writing:
- clear: simple words
- concise: sentences < 30 words
- free of jargon
- free of spelling and grammatical errors
- free of accidental offensive language.

To do this:

macOS
```bash
vale --minAlertLevel warning docs
```

macOS
```bash
vale --minAlertLevel suggestion docs
```

