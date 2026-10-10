scoreboard players add @s kw_t 1
tp @s ^ ^ ^0.25
execute if score @s kw_t matches 12.. run function kestrel_test:flip
