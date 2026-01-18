export interface LocationDto {
	srid: number | null;
	longitude: number | null;
	latitude: number | null;
}

export interface AddressDto {
	fullAddressString: string | null;
	street: string | null;
	houseNumber: string | null;
	postalCode: string | null;
	city: string | null;
	countryCode: string | null;
	osmId: number | null;
	location: LocationDto | null;
	distanceToNearestStation: number | null;
}

export interface RealEstateDto {
	id: string | null;
	externalUrl: string | null;
	title: string | null;
	price: number | null;
	pricePerM2: number | null;
	livingArea: number | null;
	totalArea: number | null;
	roomCount: number | null;
	bedroomCount: number | null;
	bathroomCount: number | null;
	source: string | null;
	timestampFound: string | null;
	address: AddressDto | null;
}
