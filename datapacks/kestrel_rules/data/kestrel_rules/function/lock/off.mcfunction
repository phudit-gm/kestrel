# Switch off Kestrel: target boxes and locking (also turns off prediction and Mode B)
team add ks_off_lock
tellraw @a[tag=!kestrel_quiet] {"text":"[Kestrel] Disabled on this server: target boxes and locking (also turns off prediction and Mode B)","color":"yellow"}
