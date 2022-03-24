# PrivateChatParser

## Usage
`java -jar PrivateChatParser.jar <input.(txt|log|log.gz)>`

```
Arguments:
    input -> Input file { File }
Options:
    --output-text, -t [output.txt] -> Output text (easily readable by human) file { File }
    --output-json, -j [output.json] -> Output JSON file { File }
    --filter, -f [] -> Apply filter { String }
    --append, -a [false] -> Append to output file instead of overwriting
    --help, -h -> Usage info
```

⚠️ This application overwrites the output-text and output-json in the working directory, without warning.

Examples of `filter` option:
- `--filter="sender = player1 || recipient = player1"`
- `--filter="sender != player1 && sender != player2"`
- `-f "(sender == player1 || recipient == player1) && is_reply == false"`

## Compiling (and running)

1. Clone the project
2. Run `gradlew shadowJar`
3. Run `java -jar build/libs/PrivateChatParser.jar <input.(txt|log|log.gz)>`
