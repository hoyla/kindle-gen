# Kindle-gen

Publishing our print-sent content to the Amazon Kindle API.

# Architecture

![Architecture diagram](docs/kindle-gen-diagram.png)

# Quick Reference

```sh
# Run the tests
sbt test
```

# Prerequisites for development 

In order to run locally, you need to supply an API key in the `content-api.key` configuration property. 
You can get a key from [Bonobo](https://bonobo.capi.gutools.co.uk). You can supply the key dynamically using:
```sh
sbt -Dcontent-api.key="<your-key>" [yourCommand]
```


# To Do

- switch all config to encrypted lambda variables during the set up of the lambda.


