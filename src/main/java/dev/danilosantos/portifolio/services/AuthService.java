package dev.danilosantos.portifolio.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import dev.danilosantos.portifolio.dto.AuthRegisterDTO;
import dev.danilosantos.portifolio.dto.AuthRequestDTO;
import dev.danilosantos.portifolio.dto.AuthResponseDTO;
import dev.danilosantos.portifolio.dto.UserDTO;
import dev.danilosantos.portifolio.entities.Token;
import dev.danilosantos.portifolio.entities.User;
import dev.danilosantos.portifolio.enums.Role;
import dev.danilosantos.portifolio.enums.TokenType;
import dev.danilosantos.portifolio.repositories.TokenRepository;
import dev.danilosantos.portifolio.repositories.UserRepository;

@Service
public class AuthService {
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private TokenRepository tokenRepository;
	
	@Autowired
    private PasswordEncoder passwordEncoder;
	
	@Autowired
    private JwtService jwtService;
	
	@Autowired
	private AuthenticationManager authenticationManager;
	

	public AuthResponseDTO register(AuthRegisterDTO obj) {
		if(!userRepository.findByEmail(obj.getEmail()).isEmpty()) {
			throw new ResourceAccessException("User alredy exist.");
		}
		
		User entity = new User();
		copyDtoToEntity(obj, entity);
		entity.setPassword(passwordEncoder.encode(obj.getPassword()));
		entity.setRole(Role.USER);
		var savedUser = userRepository.save(entity);
		var jwtToken = jwtService.generateToken(entity);
		saveUserToken(savedUser, jwtToken);
		return new AuthResponseDTO(jwtToken, entity.getId(), entity.getName());
	}

	public AuthResponseDTO login(AuthRequestDTO obj) {
		authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(obj.getEmail(), obj.getPassword()));
		var user = userRepository.findByEmail(obj.getEmail()).orElseThrow();
		var jwtToken = jwtService.generateToken(user);
		revokeAllUserToken(user);
		saveUserToken(user, jwtToken);
		return new AuthResponseDTO(jwtToken, user.getId(), user.getName());
	}

	private void revokeAllUserToken(User user) {
		var validUserTokens = tokenRepository.findAllValidTokensByUser(user.getId());
		if(validUserTokens.isEmpty()) {
			return;
		}
		validUserTokens.forEach(t -> t.setExpired(true));
		tokenRepository.saveAll(validUserTokens);
	}
	
	private void saveUserToken(User user, String jwtToken) {
		var token = new Token(null, jwtToken, TokenType.BEARER, false, user);
		tokenRepository.save(token);
	}
	
	private void copyDtoToEntity(UserDTO dto, User entity) {
		entity.setId(dto.getId());
		entity.setName(dto.getName());
		entity.setEmail(dto.getEmail());
	}
}
