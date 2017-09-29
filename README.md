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

In order to run locally you must have a special 'kindle-gen.conf' file at: `~/.gu/kindle-gen.conf`
This should contain an api key which you can get from a member of the CAPI team. Alternatively, you can run with limited functionality by creating the file with the single (and only) line which reads:
```text
test
```

# To Do

- create a sample conf file for version control if/when config becomes more compiles
- switch all config to encrypted lambda variables during the set up of the lambda.


