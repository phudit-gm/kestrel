# Switch off Kestrel: predicted position / ghost and lead (also turns off Mode B)
team add ks_off_predict
tellraw @a[tag=!kestrel_quiet] {"text":"[Kestrel] Disabled on this server: predicted position / ghost and lead (also turns off Mode B)","color":"yellow"}
