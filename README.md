# ActiveRewards

## Configuration


```yaml
# Set to true to spam console with useful information for debugging. Will even print out the evaluation of conditions and raw SQL queries!
debug: false
# Actions list.
actions:
  # This name can be anything, just has to be unique
  action1:
    # Define the conditions for this action to run
    # ALL conditions must follow this format. :default specifies from WHAT servers.
    # Put no colon [newpoints] for ALL, or specify a list seperated by commas [newpoints:default,server2,server3]
    # Valid types: newpoints, lastpoints, newhistoricalpoints, lasthistoricalpoints, newplaytime, lastplaytime
    # Valid operations: <, >, <=, >=, =
    # The end of the string must be an integer between 0 and Integer.MAX_VALUE
    # last(x): The value of this last time the server was running and the player was online
    # new(x): The new value that will be inserted into last(x) after conditions execute
    if:
    - '[newpoints:default]>=2'
    - '[lastpoints:default]<2'
    # A list of commands to run should all conditions above evaluate to true. Replaces {username} with the player name it evaluates on.
    do:
    - lp user {username} parent settrack playtime lvl1
  action2:
    if:
    - '[newpoints:default]<2'
    - '[lastpoints:default]>=2'
    do:
    - lp user {username} parent settrack playtime default
# If set to false, /ar info will only display total points/playtime and not from which servers.
show_multiple_servers: true
# Allows you to set points multipliers based on permissions. A multiplier of 2 = 2x points per minute.
modifiers:
  default:
    permission: activerewards.use
    multiplier: 1
# The server name for this server, displayed in ar info and used in parsing actions, as well as in the database.
server: default
# SQL Host Specification. If you are on a multi-server network, use the same database. You MUST specify a database.
sql:
  host: 
  port: 3306
  database: activerewards
  username: 
  password: 
  prefix: ar_
  use_ssl: false
```