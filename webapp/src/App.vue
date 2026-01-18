<template>
	<router-view />
</template>

<script lang="ts" setup>
import { onMounted } from "vue";
import { useGeoStore } from "stores/geoStore";

const geoStore = useGeoStore();

onMounted(async () => {
	console.log("App mounted successfully!");
	await geoStore.loadGeoData();
});
</script>
