scoreboard players add @s kw_t 1
tp @s ^ ^ ^0.2
execute if score @s kw_t matches 100.. run function kestrel_test:flip
