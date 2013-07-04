META_BEAGLEBONE_ROOT=~/src/meta-beagleboard
PATCH_DIR="$META_BEAGLEBONE_ROOT"/common-bsp/recipes-kernel/linux/linux-mainline-3.8
PATCHSETS="dma rtc pinctrl cpufreq adc i2c da8xx-fb pwm mmc crypto 6lowpan capebus arm omap omap_sakoman omap_beagle_expansion omap_beagle omap_panda net drm not-capebus pru usb PG2 reboot iio w1 gpmc mxt ssd130x build hdmi resetctrl camera"
#resources pmic pps
for patchset in $PATCHSETS ; do
	fail="0"
	find $PATCH_DIR/$patchset -name '*.patch' | sort -n | while read i
	do
		git apply --whitespace=nowarn "$i" || { echo "$i" ; fail="1" ; break; }
	done
	if [ "$fail" -eq "1" ]; then
		exit 1
	fi
done
