<script lang="ts" setup>
import { LMap, LMarker, LPopup, LTileLayer } from "@vue-leaflet/vue-leaflet";
</script>

<template>
	<l-map :center="[51.505, -0.09]" :zoom="13" style="height: 500px; width: 100%">
		<l-tile-layer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
		<l-marker :lat-lng="[51.505, -0.09]">
			<l-popup>
				A pretty CSS popup.<br />
				Easily customizable.
			</l-popup>
		</l-marker>
	</l-map>
</template>

<style scoped></style>
