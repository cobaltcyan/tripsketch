package kr.kro.tripsketch.controllers

import jakarta.servlet.http.HttpServletRequest
import kr.kro.tripsketch.dto.TripIdDto
import kr.kro.tripsketch.exceptions.UnauthorizedException
import kr.kro.tripsketch.services.TripLikeService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/trip")
class TripLikeController(private val tripLikeService: TripLikeService) {

    @PostMapping("/like")
    fun likeTrip(
        req: HttpServletRequest,
        @RequestBody tripIdDto: TripIdDto,
    ): ResponseEntity<String> {
        val memberId = req.getAttribute("memberId") as Long?
            ?: throw UnauthorizedException("해당 사용자가 존재하지 않습니다.")
        return try {
            tripLikeService.likeTrip(memberId, tripIdDto.id)
            ResponseEntity.status(HttpStatus.OK).body("해당 게시물을 '좋아요'하였습니다.")
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/unlike")
    fun unlikeTrip(
        req: HttpServletRequest,
        @RequestBody tripIdDto: TripIdDto,
    ): ResponseEntity<Any> {
        val memberId = req.getAttribute("memberId") as Long?
            ?: throw UnauthorizedException("해당 사용자가 존재하지 않습니다.")
        return try {
            tripLikeService.unlikeTrip(memberId, tripIdDto.id)
            ResponseEntity.status(HttpStatus.OK).body("좋아요를 취소하였습니다.")
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/toggle-like")
    fun toggleTripLike(
        req: HttpServletRequest,
        @RequestBody tripIdDto: TripIdDto,
    ): ResponseEntity<Any> {
        val memberId = req.getAttribute("memberId") as Long?
            ?: throw UnauthorizedException("해당 사용자가 존재하지 않습니다.")
        return try {
            tripLikeService.toggleTripLike(memberId, tripIdDto.id)
            val isLiked = tripLikeService.isTripLiked(memberId, tripIdDto.id)
            val message = if (isLiked) "게시물을 '좋아요'하였습니다." else "'좋아요'를 취소하였습니다."
            ResponseEntity.status(HttpStatus.OK).body(mapOf("message" to message))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("message" to (e.message ?: "")))
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("message" to (e.message ?: "")))
        }
    }
}
