# AutoWaker
Sends Wake-on-Lan based on active devices in the network.

# Compile (Not anymore)
    ./src$ javac -cp . com.adam.wol.WakeOnLan

# MAN
```
-start [delay (seconds)]             starts the Waker | default delay = 5s
-set                                 sets trigger/target device (mac separators: '-' or ':')
-list                                list all current mappings
-l                                   same as -list
```
