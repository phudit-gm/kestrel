execute as @e[type=minecraft:husk,tag=kw_slow] at @s run function kestrel_test:step_slow
execute as @e[type=minecraft:husk,tag=kw_mid] at @s run function kestrel_test:step_mid
execute as @e[type=minecraft:husk,tag=kw_fast] at @s run function kestrel_test:step_fast
execute as @e[type=minecraft:husk,tag=kw_zig] at @s run function kestrel_test:step_zig
execute as @e[tag=kw] run data modify entity @s Health set value 1024f
