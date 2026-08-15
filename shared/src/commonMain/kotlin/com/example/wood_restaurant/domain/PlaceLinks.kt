package com.example.wood_restaurant.domain

/**
 * 장소 하나로 만들 수 있는 외부 링크들.
 *
 * 네이버 지도 앱 URL 스킴: https://guide.ncloud-docs.com/docs/maps-url-scheme
 * `appname`에는 호출하는 앱의 패키지명(Android) / 번들 ID(iOS)를 넣어야 한다.
 */
object PlaceLinks {

    /** 네이버 지도 앱으로 대중교통 길찾기. 앱이 없으면 열리지 않으므로 [webSearchUrl]을 폴백으로 쓴다. */
    fun naverMapDirections(place: Restaurant, appIdentifier: String): String =
        "nmap://route/public" +
            "?dlat=${place.position.latitude}" +
            "&dlng=${place.position.longitude}" +
            "&dname=${percentEncode(place.name)}" +
            "&appname=$appIdentifier"

    /** 네이버 지도 웹에서 상호+주소로 검색. 앱 유무와 무관하게 항상 열린다. */
    fun webSearchUrl(place: Restaurant): String =
        "https://map.naver.com/p/search/${percentEncode("${place.name} ${place.displayAddress}".trim())}"

    /** 지역검색 API가 준 장소 페이지 링크. 비어 있으면 웹 검색으로 대신한다. */
    fun placePageUrl(place: Restaurant): String =
        place.link.ifBlank { webSearchUrl(place) }

    /** 공유 시트에 넣을 본문. */
    fun shareText(place: Restaurant): String = buildString {
        appendLine(place.name)
        if (place.subCategory.isNotBlank()) appendLine(place.subCategory)
        if (place.displayAddress.isNotBlank()) appendLine(place.displayAddress)
        if (place.telephone.isNotBlank()) appendLine(place.telephone)
        append(placePageUrl(place))
    }
}

/**
 * RFC 3986 쿼리 컴포넌트용 퍼센트 인코딩. 한글은 UTF-8 바이트 단위로 인코딩된다.
 * KMP 공통 코드에는 URLEncoder가 없어서 직접 쓴다.
 */
fun percentEncode(value: String): String = buildString {
    for (byte in value.encodeToByteArray()) {
        val c = byte.toInt() and 0xFF
        val isUnreserved = c in 'A'.code..'Z'.code || c in 'a'.code..'z'.code ||
            c in '0'.code..'9'.code || c == '-'.code || c == '_'.code || c == '.'.code || c == '~'.code
        if (isUnreserved) {
            append(c.toChar())
        } else {
            append('%')
            append(HEX[c shr 4])
            append(HEX[c and 0x0F])
        }
    }
}

private const val HEX = "0123456789ABCDEF"
